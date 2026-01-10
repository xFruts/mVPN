export interface ApiResponse<T = unknown> {
    data: T;
    success: boolean;
    message?: string;
    errors?: string[];
}

export interface ApiError {
    statusCode: number;
    message: string;
    details?: unknown;
}
