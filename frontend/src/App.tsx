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
import ModelLabPage from "./pages/ModelLabPage";
import GlossaryPage from "./pages/GlossaryPage";
import GlossaryDetailPage from "./pages/GlossaryDetailPage";
import MyPage from "./pages/MyPage";
import AdminGlossaryPage from "./pages/AdminGlossaryPage";

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/history" element={<ProtectedRoute><HistoryPage /></ProtectedRoute>} />
        <Route path="/learn" element={<ProtectedRoute><LearnPage /></ProtectedRoute>} />
        <Route path="/practice" element={<ProtectedRoute><PracticePage /></ProtectedRoute>} />
        <Route path="/models" element={<ProtectedRoute><ModelLabPage /></ProtectedRoute>} />
        <Route path="/glossary" element={<GlossaryPage />} />
        <Route path="/glossary/:slug" element={<GlossaryDetailPage />} />
        <Route path="/me" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
        <Route path="/admin/glossary" element={<ProtectedRoute><AdminGlossaryPage /></ProtectedRoute>} />
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
