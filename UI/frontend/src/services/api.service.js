import instance from "./axios.customize";
const createUserAPI = (fullName, password, email, phone) => {
    const URL_API = "/api/v1/user";
    const data = {
        fullName,
        password,
        email,
        phone
    };
    return instance.post(URL_API, data);
};

const fetchAllUsersAPI = () => {
    const URL_API = "/api/v1/user"
    return instance.get(URL_API);
}

const updateUserAPI = (_id, fullName, phone) => {
    const URL_API = "/api/v1/user"
    const data = {
        _id: _id,
        fullName: fullName,
        phone: phone
    }
    return instance.put(URL_API, data)

}

// ---------- 3.3.1 GET /api/v1/sensors/latest ----------
const getLatestSensorAPI = () => {
    return instance.get("/api/v1/sensors/latest");
};

// ---------- 3.3.2 GET /api/v1/sensors/chart ----------
const getSensorChartAPI = (params = {}) => {
    return instance.get("/api/v1/sensors/chart", { params });
};

// ---------- 3.3.4 GET /api/v1/sensors/sensor-data ----------
const getSensorDataAPI = (params = {}) => {
    return instance.get("/api/v1/sensors/sensor-data", { params });
};

// ---------- GET /api/v1/devices ----------
const getDevicesAPI = () => {
    return instance.get("/api/v1/devices");
};

// ---------- 3.3.3 POST /api/v1/devices/control ----------
const controlDeviceAPI = (deviceId, action, userId) => {
    const data = { device_id: deviceId, action };
    if (userId != null) data.user_id = userId;
    return instance.post("/api/v1/devices/control", data);
};

// ---------- 3.3.5 GET /api/v1/actions/history ----------
const getActionHistoryAPI = (params = {}) => {
    return instance.get("/api/v1/actions/history", { params });
};

export {
    createUserAPI,
    fetchAllUsersAPI,
    updateUserAPI,
    getLatestSensorAPI,
    getSensorChartAPI,
    getSensorDataAPI,
    getDevicesAPI,
    controlDeviceAPI,
    getActionHistoryAPI
};
