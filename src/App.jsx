import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { Toaster } from 'react-hot-toast';
import Layout from './components/layout/Layout';
import ProtectedRoute from './components/auth/ProtectedRoute';
import AiChatWidget from './components/ai/AiChatWidget';
import LoginPage from './features/auth/pages/LoginPage';
import Home from './features/dashboard/pages/Home';
import DashboardPage from './features/dashboard/pages/DashboardPage';
import Step1PersonalRisk from './features/assessment/pages/Step1PersonalRisk';
import Step2IncomeExpenses from './features/assessment/pages/Step2IncomeExpenses';
import Step3AssetsLiabilities from './features/assessment/pages/Step3AssetsLiabilities';
import Step4FinancialGoals from './features/assessment/pages/Step4FinancialGoals';
import Step5InsuranceGap from './features/assessment/pages/Step5InsuranceGap';
import Step6TaxOptimization from './features/assessment/pages/Step6TaxOptimization';
import AssessmentComplete from './features/assessment/pages/AssessmentComplete';
import AdminDashboard from './features/admin/pages/AdminDashboard';

const queryClient = new QueryClient();

function ConditionalChatWidget() {
    const { pathname } = useLocation();
    if (pathname.startsWith('/admin') || pathname === '/login') return null;
    return <AiChatWidget />;
}

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

export default function App() {
  return (
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
            <Routes>
              {/* Public route */}
              <Route path="/login" element={<LoginPage />} />

              {/* All routes require SSO login */}
              <Route element={<ProtectedRoute />}>
                {/* Admin — full page, no Layout wrapper */}
                <Route path="/admin" element={<AdminDashboard />} />

                <Route element={<Layout />}>
                  <Route path="/" element={<Home />} />
                  <Route path="/assessment/step-1" element={<Step1PersonalRisk />} />
                  <Route path="/assessment/step-2" element={<Step2IncomeExpenses />} />
                  <Route path="/assessment/step-3" element={<Step3AssetsLiabilities />} />
                  <Route path="/assessment/step-4" element={<Step4FinancialGoals />} />
                  <Route path="/assessment/step-5" element={<Step5InsuranceGap />} />
                  <Route path="/assessment/step-6" element={<Step6TaxOptimization />} />
                  <Route path="/assessment/complete" element={<AssessmentComplete />} />
                  <Route path="/dashboard" element={<DashboardPage />} />
                </Route>
              </Route>
            </Routes>
            <ConditionalChatWidget />
        </BrowserRouter>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3500,
            style: {
              background: 'rgba(15, 23, 42, 0.85)',
              backdropFilter: 'blur(16px)',
              WebkitBackdropFilter: 'blur(16px)',
              color: '#e2e8f0',
              border: '1px solid rgba(255,255,255,0.08)',
              borderLeft: '3px solid #0DF259',
              borderRadius: '14px',
              fontSize: '13px',
              padding: '14px 18px',
              boxShadow: '0 8px 32px rgba(0,0,0,0.4), 0 0 20px rgba(13,242,89,0.08)',
              maxWidth: '380px',
            },
            error: {
              iconTheme: { primary: '#fbbf24', secondary: 'rgba(15, 23, 42, 0.85)' },
              style: {
                borderLeft: '3px solid #fbbf24',
                boxShadow: '0 8px 32px rgba(0,0,0,0.4), 0 0 20px rgba(251,191,36,0.08)',
              },
            },
            success: {
              iconTheme: { primary: '#0DF259', secondary: 'rgba(15, 23, 42, 0.85)' },
            },
          }}
        />
      </QueryClientProvider>
    </GoogleOAuthProvider>
  );
}
