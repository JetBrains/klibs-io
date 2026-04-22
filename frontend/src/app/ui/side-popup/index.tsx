import React, {ReactNode, useState} from 'react';
import styles from './styles.module.css';

interface SidePopupProps {
    children: ReactNode;
    target: ReactNode;
}

const SidePopup: React.FC<SidePopupProps> = ({ children, target }) => {

    const [isVisible, setIsVisible] = useState(false);
    const handleMouseEnter = () => {
        setIsVisible(true);
    };
    const handleMouseLeave = () => {
        setIsVisible(false);
    };

    return (
        <div className={styles.wrapper}>
            {/* Target element */}
            <div
                onMouseEnter={handleMouseEnter}
                onMouseLeave={handleMouseLeave}
                className={styles.targetElement}
            >
                {target}
            </div>

            {/* Popup */}
            {isVisible && (
                <div className={styles.popup}>
                    {children}
                </div>
            )}
        </div>
    );
};

export default SidePopup;
