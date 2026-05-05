"use client"

import React, {useCallback, useEffect, useState} from 'react';
import Image from 'next/image';
import styles from './styles.module.css';

interface GitHubRepo {
    name: string;
    html_url: string;
    stargazers_count: number;
}

export function UserWidget() {
    const [login, setLogin] = useState<string | null>(null);
    const [topRepo, setTopRepo] = useState<GitHubRepo | null>(null);
    const [ready, setReady] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);

    useEffect(() => {
        fetch('/auth/me', {cache: 'no-store'})
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                if (data?.login) {
                    setLogin(data.login);
                    return fetch(
                        `https://api.github.com/users/${data.login}/repos?type=public&sort=stars&direction=desc&per_page=1`
                    )
                        .then(r => r.json())
                        .then(repos => {
                            if (Array.isArray(repos) && repos[0]) setTopRepo(repos[0]);
                        });
                }
            })
            .catch(() => {})
            .finally(() => setReady(true));
    }, []);

    const logout = useCallback(() => {
        fetch('/auth/logout', {method: 'POST'})
            .finally(() => window.location.reload());
    }, []);

    useEffect(() => {
        if (!menuOpen) return;
        const close = () => setMenuOpen(false);
        document.addEventListener('click', close);
        return () => document.removeEventListener('click', close);
    }, [menuOpen]);

    if (!ready) return null;

    if (!login) {
        return (
            <a href="/auth/github/login" className={styles.loginBtn}>
                Sign in with GitHub
            </a>
        );
    }

    return (
        <div className={styles.userWidget}>
            {topRepo && (
                <a
                    href={topRepo.html_url}
                    target="_blank"
                    rel="noreferrer"
                    className={styles.topRepo}
                >
                    ★ {topRepo.name}
                    <span className={styles.starCount}>{topRepo.stargazers_count.toLocaleString()}</span>
                </a>
            )}

            <button
                className={styles.userBtn}
                onClick={e => { e.stopPropagation(); setMenuOpen(o => !o); }}
                aria-label="User menu"
            >
                <Image
                    src={`https://avatars.githubusercontent.com/${login}`}
                    alt={login}
                    width={28}
                    height={28}
                    className={styles.avatar}
                    unoptimized
                />
                <span className={styles.userLogin}>{login}</span>
                <span className={styles.chevron}>▾</span>
            </button>

            {menuOpen && (
                <div className={styles.userMenu} onClick={e => e.stopPropagation()}>
                    <button className={styles.userMenuItem} onClick={logout}>
                        Sign out
                    </button>
                </div>
            )}
        </div>
    );
}
