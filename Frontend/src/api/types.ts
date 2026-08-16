export interface ApiResponse<T = unknown> {
    content: T;
    success: boolean;
    message?: string;
    errors?: string[];
}

export interface ApiErrorType {
    statusCode: number;
    message: string;
    details?: unknown;
}
