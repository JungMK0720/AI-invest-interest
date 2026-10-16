import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import HomePage from "./pages/HomePage";
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
import AdminLearningPage from "./pages/AdminLearningPage";

/**
 * 학습·용어·모의투자·모델 실험실은 진단 등급이나 로그인과 무관하게 열어 둔다(docs/09).
 * 진단·이력·내 학습·관리 화면만 로그인이 필요하다.
 */
export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/glossary" element={<GlossaryPage />} />
        <Route path="/glossary/:slug" element={<GlossaryDetailPage />} />
        <Route path="/learn" element={<LearnPage />} />
        <Route path="/practice" element={<PracticePage />} />
        <Route path="/models" element={<ModelLabPage />} />

        <Route path="/start" element={<ProtectedRoute><StartPage /></ProtectedRoute>} />
        <Route path="/diagnosis" element={<ProtectedRoute><QuestionPage /></ProtectedRoute>} />
        <Route path="/result/:assessmentId" element={<ProtectedRoute><ResultPage /></ProtectedRoute>} />
        <Route path="/explore/:assessmentId" element={<ProtectedRoute><ExplorePage /></ProtectedRoute>} />
        <Route path="/product/:productId" element={<ProtectedRoute><ProductDetailPage /></ProtectedRoute>} />
        <Route path="/history" element={<ProtectedRoute><HistoryPage /></ProtectedRoute>} />
        <Route path="/me" element={<ProtectedRoute><MyPage /></ProtectedRoute>} />
        <Route path="/admin" element={<Navigate to="/admin/glossary" replace />} />
        <Route path="/admin/glossary" element={<ProtectedRoute><AdminGlossaryPage /></ProtectedRoute>} />
        <Route path="/admin/learning" element={<ProtectedRoute><AdminLearningPage /></ProtectedRoute>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
