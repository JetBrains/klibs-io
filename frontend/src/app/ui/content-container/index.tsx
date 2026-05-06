import styles from './styles.module.css';
import React from "react";

export default function ContentContainer({ children }: { children: React.ReactNode }) {
	return (
		<div className={styles.container}>
			{children}
		</div>
	);
}