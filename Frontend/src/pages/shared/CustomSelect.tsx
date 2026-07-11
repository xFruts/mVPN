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
}

const minimalStyles: StylesConfig<SelectOption, false> = {
    control: (base, state) => ({
        ...base,
        backgroundColor: "var(--card-bg)",
        borderColor: state.isFocused
            ? "var(--card-border-focus)"
            : "var(--card-border)",
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
            borderColor: state.isFocused
                ? "var(--card-border-focus)"
                : "var(--card-border)",
        },
    }),

    menu: (base) => ({
        ...base,
        backgroundColor: "var(--card-bg)",
        border: "1px solid var(--card-border)",
        borderRadius: "8px",
        zIndex: 9999,
        boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)",
        overflow: "hidden",
    }),

    menuList: (base) => ({
        ...base,
        backgroundColor: "var(--card-bg)",
        opacity: 1,
        padding: 0,
        borderRadius: "8px",
        "&::-webkit-scrollbar": {
            width: "4px",
        },
        "&::-webkit-scrollbar-thumb": {
            backgroundColor: "var(--card-border)",
            borderRadius: "10px",
        },
    }),

    option: (base, { isFocused, isSelected }) => ({
        ...base,
        backgroundColor: isSelected
            ? "rgba(6, 182, 212, 0.3)"
            : isFocused
              ? "rgba(255, 255, 255, 0.05)"
              : "var(--card-bg)",
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
};

export const MySelect = ({
    options,
    value,
    onChange,
    placeholder,
}: MySelectProps) => {
    return (
        <Select
            options={options}
            styles={minimalStyles}
            placeholder={placeholder}
            isSearchable={false}
            value={options.find((opt) => opt.value === value) || null}
            onChange={(val) => onChange(val ? val.value : null)}
        />
    );
};
