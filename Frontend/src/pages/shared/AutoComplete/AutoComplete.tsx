import { useState, useRef, useEffect } from "react";
import styles from "./AutoComplete.module.css";
import { usePayingStore } from "@store/usePayingStore.ts";
import type { AllUsers } from "@/types/user.ts";
import { UserService } from "@api/services/userService.ts";



export default function Autocomplete() {
    const { setUserId } = usePayingStore();
    const [users, setUsers] = useState<AllUsers[]>([]);
    const [query, setQuery] = useState("");
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const wrapperRef = useRef<HTMLDivElement>(null);

    const filteredUsers = users.filter((user) =>
        user.fullName.toLowerCase().includes(query.toLowerCase()),
    );

    useEffect(() => {
        try {
            UserService.getAllUsers().then((data) => {
                setUsers(data);
            })
        }
        catch(e) {
            console.error(e);
        }
    }, []);

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (
                wrapperRef.current &&
                !wrapperRef.current.contains(event.target as Node)
            ) {
                setIsMenuOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () =>
            document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    return (
        <div className={styles.container} ref={wrapperRef}>
            <input
                type="text"
                className={styles.input}
                placeholder="Введите имя пользователя..."
                value={query}
                onChange={(e) => {
                    setQuery(e.target.value);
                    setIsMenuOpen(true);
                }}
                onFocus={() => setIsMenuOpen(true)}
            />

            {isMenuOpen && filteredUsers.length > 0 && (
                <ul className={styles.dropdown}>
                    {filteredUsers.map((user) => (
                        <li
                            key={user.id}
                            className={styles.item}
                            onMouseDown={(e) => {
                                e.preventDefault();
                            }}
                            onClick={(e) => {
                                e.stopPropagation();
                                setQuery(user.fullName);
                                setUserId(user.id);
                                setIsMenuOpen(false);
                            }}
                        >
                            {user.fullName} (ID: {user.id})
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
}
