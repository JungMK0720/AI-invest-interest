import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import HistoryPage from "./pages/HistoryPage";
import LearnPage from "./pages/LearnPage";
import PracticePage from "./pages/PracticePage";
import LoginPage from "./pages/LoginPage";
import StartPage from "./pages/StartPage";
import QuestionPage from "./pages/QuestionPage";
import ResultPage from "./pages/ResultPage";
import ExplorePage from "./pages/ExplorePage";
import ProductDetailPage from "./pages/ProductDetailPage";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/history" element={<ProtectedRoute><HistoryPage /></ProtectedRoute>} />
        <Route path="/learn" element={<ProtectedRoute><LearnPage /></ProtectedRoute>} />
        <Route path="/practice" element={<ProtectedRoute><PracticePage /></ProtectedRoute>} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/start"
          element={
            <ProtectedRoute>
              <StartPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/diagnosis"
          element={
            <ProtectedRoute>
              <QuestionPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/result/:assessmentId"
          element={
            <ProtectedRoute>
              <ResultPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/explore/:assessmentId"
          element={
            <ProtectedRoute>
              <ExplorePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/product/:productId"
          element={
            <ProtectedRoute>
              <ProductDetailPage />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/start" replace />} />
        <Route path="*" element={<Navigate to="/start" replace />} />
      </Routes>
    </AuthProvider>
  );
}
