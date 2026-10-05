let current = { pathname: '/', search: '' };

export function setNavigation(next: { pathname: string; search?: string }) {
    current = { pathname: next.pathname, search: next.search ?? '' };
}

export const usePathname = () => current.pathname;
export const useSearchParams = () => new URLSearchParams(current.search);
export const useRouter = () => ({ back: () => {}, push: () => {}, refresh: () => {}, replace: () => {} });
