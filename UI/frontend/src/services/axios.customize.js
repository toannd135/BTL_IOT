import axios from "axios";

const instance = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL || "http://localhost:8080",
    headers: {
        "Content-Type": "application/json",
    },
});

instance.interceptors.request.use(
    (config) => {
        console.log("Request:", {
            method: config.method,
            url: config.url,
            data: config.data,
        });

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

instance.interceptors.response.use(
    (response) => {
        return response.data;
    },
    (error) => {
        console.error("API Error:", error);

        const errorData = error?.response?.data || {
            message: error?.message || "Đã có lỗi kết nối tới server!",
        };

        return Promise.reject(errorData);
    }
);

export default instance;