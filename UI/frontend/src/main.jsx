import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import App from './App'
import Dashboard from './pages/Dashboard';
import SensorData from './pages/SensorData';
import History from './pages/History';
import Profile from './pages/Profile';

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      {
        index: true,
        element: <Dashboard />
      },
      {
        path: "datasensor",
        element: <SensorData />
      },
      {
        path: "history",
        element: <History />
      },
      {
        path: "profile",
        element: <Profile />
      }
    ]
  }
]);

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
