import Select, { type StylesConfig } from "react-select";

export interface SelectOption {
    value: string | number;
    label: string;
}

interface MySelectProps {
    options: SelectOption[];
    value?: string | number | null;
    onChange: (value: string | number | null) => void;
    placeholder?: string;
    backgroundColor?: string; // Проп для фона
    borderColor?: string; // Проп для границы (обычное состояние)
}

// Функция-генератор стилей принимает и фон, и цвет границы
const getSelectStyles = (
    bgColor?: string,
    bColor?: string,
): StylesConfig<SelectOption, false> => ({
    control: (base, state) => ({
        ...base,
        backgroundColor: bgColor || "var(--card-bg)",
        // Если в фокусе - всегда синий/акцентный, если нет - берем проп или дефолт
        borderColor: state.isFocused
            ? "var(--card-border-focus)"
            : bColor || "var(--card-border)",
        color: "var(--text-secondary)",
        height: "44px",
        minHeight: "44px",
        display: "flex",
        alignItems: "center",
        flexWrap: "nowrap",
        borderRadius: "12px",
        boxShadow: "none",
        padding: "0 8px",
        "&:hover": {
            // Аналогичная логика для ховера
            borderColor: state.isFocused
                ? "var(--card-border-focus)"
                : bColor || "var(--card-border)",
        },
    }),

    menu: (base) => ({
        ...base,
        backgroundColor: bgColor || "var(--card-bg)",
        border: `1px solid ${bColor || "var(--card-border)"}`,
        borderRadius: "8px",
        zIndex: 9999,
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
        padding: 0,
        overflow: "hidden",
    }),

    menuList: (base) => ({
        ...base,
        backgroundColor: bgColor || "var(--card-bg)",
        padding: 0,
        borderRadius: "8px",
        "&::-webkit-scrollbar": {
            width: "4px",
        },
        "&::-webkit-scrollbar-thumb": {
            backgroundColor: bColor || "var(--card-border)",
            borderRadius: "10px",
        },
    }),

    option: (base, { isFocused, isSelected }) => ({
        ...base,
        backgroundColor: isSelected
            ? "rgba(6, 182, 212, 0.3)"
            : isFocused
              ? "rgba(255, 255, 255, 0.05)"
              : bgColor || "var(--card-bg)",
        color: isSelected ? "#06b6d4" : "var(--text-secondary)",
        cursor: "pointer",
        padding: "10px 12px",
        "&:active": {
            backgroundColor: "rgba(6, 182, 212, 0.4)",
        },
    }),

    placeholder: (base) => ({
        ...base,
        color: "var(--text-secondary)",
        margin: 0,
        padding: 0,
    }),

    singleValue: (base) => ({
        ...base,
        color: "var(--text-secondary)",
    }),

    indicatorSeparator: () => ({ display: "none" }),

    dropdownIndicator: (base) => ({
        ...base,
        color: "var(--text-secondary)",
    }),
});

export const MySelect = ({
    options,
    value,
    onChange,
    placeholder,
    backgroundColor,
    borderColor,
}: MySelectProps) => {
    // Передаем оба параметра в генератор стилей
    const styles = getSelectStyles(backgroundColor, borderColor);

    return (
        <Select
            options={options}
            styles={styles}
            placeholder={placeholder}
            isSearchable={false}
            value={options.find((opt) => opt.value === value) || null}
            onChange={(val) => onChange(val ? val.value : null)}
        />
    );
};
