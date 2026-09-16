interface Window {
    HospitalValidation: {
        validate(root: Element): boolean;
        message(field: HTMLInputElement): string;
        applyServerErrors(errors: Record<string, string>): void;
    };
}
