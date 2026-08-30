import React from "react"; // Импортируем React для доступа к типам
import "./Logo.css";

interface LogoProps {
    size?: number;
}

export default function Logo({ size = 36 }: LogoProps) {
    return (
        <div
            className="logo-wrapper"
            style={
                {
                    "--logo-size": `${size}px`,
                } as React.CSSProperties
            }
        >
            <div className="bar b1"></div>
            <div className="bar b2"></div>
            <div className="bar b3"></div>
            <div className="bar b4"></div>
            <div className="bar b5"></div>
        </div>
    );
}
