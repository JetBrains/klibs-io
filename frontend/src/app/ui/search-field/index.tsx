import styles from './styles.module.css';
import { SearchIcon } from "@rescui/icons";
import React, { useCallback, useEffect, useRef, useState } from "react";

import cn from "classnames";
import { ProjectSearchResults } from "@/app/types";
// import SearchSuggestionsList from "@/app/ui/search-suggestions/search-suggestions";
import { Tag, presets } from '@rescui/tag';
import { CloseIcon } from '@rescui/icons';

import { trackEvent, GAEvent } from "@/app/analytics";

interface SearchFieldProps {
	onChange?: (value: string) => void;
	value?: string;
	onEnter?: (value: string) => void;
	className?: string;
	autofocus?: boolean;
	suggestionsList?: ProjectSearchResults[] | null
	suggestionsClose?: () => void;
	compact?: boolean;
	selectedCategory?: string | null;
	onCategoryReset?: () => void;
	onClear?: () => void;
	projectsCount?: string;
}

export default function SearchField({ value, onChange, onEnter, className, autofocus, suggestionsList, suggestionsClose, compact, selectedCategory, onCategoryReset, onClear, projectsCount }: SearchFieldProps) {
	const [isFocused, setFocused] = useState(false);
	const inputRef = useRef<HTMLInputElement>(null);
	const [inputValue, setInputValue] = useState(value || "");
	const debounceTimeout = useRef<number | null>(null);

	useEffect(() => {
		if ((value ?? "") !== inputValue) {
			setInputValue(value || "");
		}
	}, [value]);

	// Debounced onChange
	useEffect(() => {
		if (debounceTimeout.current !== null) {
			clearTimeout(debounceTimeout.current);
		}

		debounceTimeout.current = window.setTimeout(() => {
			if (onChange && inputValue !== value) {
				onChange(inputValue);
			}
		}, 200);

		return () => {
			if (debounceTimeout.current !== null) {
				clearTimeout(debounceTimeout.current);
			}
		};
	}, [inputValue, onChange, value]);

	const setFocus = () => {
		if (inputRef.current) {
			inputRef.current?.focus();
			const valueLength = inputRef.current.value.length;
			inputRef.current.setSelectionRange(valueLength, valueLength);
		}
	};

	// Autofocus only on initial load
	useEffect(() => {
		if (autofocus) {
			setFocus();
		}
	}, [autofocus]);

	useEffect(() => {
		if (typeof window !== "undefined") {

			const handleShortcut = (event: KeyboardEvent) => {
				if (event.code === "Slash") {
					if (!isFocused) {
						event.preventDefault();
					}
					trackEvent(GAEvent.SEARCH_KEYBOARD_TRIGGER, {});
					setFocus();
				}
			};

			window.addEventListener("keydown", handleShortcut);

			return () => {
				window.removeEventListener("keydown", handleShortcut);
			};
		}
	}, [isFocused]);


	const clearSuggestionsList = useCallback(() => {
		setInputValue("");
		suggestionsClose && suggestionsClose();
	}, [setInputValue, suggestionsClose])

	const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
		if (event.key === 'Enter') {
			onEnter && onEnter(event.currentTarget.value);
		} else if (event.key === 'Escape') {
			inputRef.current?.blur();
			clearInput();
		}
	};

	const handleFocus = () => {
		setFocused(true);
	};

	const handleBlur = () => {
		setFocused(false);
	};

	const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
		setInputValue(e.target.value);
	};

	const handleClearButtonClick = (e: React.PointerEvent<HTMLDivElement>) => {
		e.preventDefault();
		clearInput();
	}

	const clearInput = () => {
		setInputValue("");
		if (suggestionsList) {
			clearSuggestionsList();
		}
		onClear?.();
		setFocus();
	}

	const handleCategoryReset = (e: React.PointerEvent<HTMLDivElement> | React.KeyboardEvent<HTMLDivElement>) => {
		if (e.type === 'keydown') {
			const keyEvent = e as React.KeyboardEvent;
			if (keyEvent.key !== 'Enter' && keyEvent.key !== ' ') return;
		}
		e.preventDefault();
		onCategoryReset?.();
	}

	// In category view, show clear button below input; otherwise inside the field
	const showClearButtonInside = !selectedCategory && isFocused && inputValue.length > 0;
	const showClearButtonBelow = selectedCategory && isFocused && inputValue.length > 0;

	return (
		<div className={cn(styles.searchFieldWrapper, className)}>
			<div className={cn(styles.searchField, {
				[styles.searchFieldCompact]: compact
			})}>

				<div className={styles.leftIcon}>
					<SearchIcon />
				</div>

				<input
					data-testid="search-input"
					type="text"
					className={cn(styles.input, { [styles.showPlaceholder]: selectedCategory })}
					placeholder={selectedCategory ? `Search in ${selectedCategory}` : `Search ${projectsCount}+ KMP projects`}
					value={inputValue}
					onChange={handleChange}
					onKeyDown={handleKeyDown}
					onFocus={handleFocus}
					onBlur={handleBlur}
					ref={inputRef}
				/>

				{!isFocused && !inputValue && !selectedCategory && (
					<div className={styles.shortcut}>
						Press <Tag {...presets['outline-dark']}>/</Tag> to search {projectsCount}+ KMP projects
					</div>
				)}

				{selectedCategory && (
					<div
						className={cn(styles.shortcut, styles.clickable, styles.categoryTag)}
						onPointerDown={handleCategoryReset}
						onKeyDown={handleCategoryReset}
						tabIndex={0}
						role="button"
						aria-label={`Clear ${selectedCategory} filter`}
						data-testid="category-clear-tag"
					>
						<Tag {...presets['outline-dark']} iconPosition='right' icon={<CloseIcon />}> { selectedCategory }</Tag>
					</div>
				)}

			{showClearButtonInside && (
				<div className={cn(styles.shortcut, styles.clickable)} onPointerDown={handleClearButtonClick}>
					Clear <Tag {...presets['outline-dark']}>Esc</Tag>
				</div>
			)}

			{/*Suggestion list hidden until fixed*/}
			{/*Also, seams like it is a bit broken in FF*/}
			{/*{!!suggestionsList?.length && inputValue.length > 1 && <SearchSuggestionsList list={suggestionsList} onEnter={onEnter} suggestionsClose={suggestionsClose} />}*/}

		</div>

			{
		showClearButtonBelow && (
			<div className={styles.clearHint} onPointerDown={handleClearButtonClick}>
				Clear <Tag {...presets['outline-dark']}>Esc</Tag>
			</div>
		)
	}
		</div >
	)
}
