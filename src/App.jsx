import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/Layout';
import Home from './pages/Home';
import Step1PersonalRisk from './pages/Step1PersonalRisk';
import Step2IncomeExpenses from './pages/Step2IncomeExpenses';
import Step3AssetsLiabilities from './pages/Step3AssetsLiabilities';
import Step4FinancialGoals from './pages/Step4FinancialGoals';
import Step5InsuranceGap from './pages/Step5InsuranceGap';
import Step6TaxOptimization from './pages/Step6TaxOptimization';
import AssessmentComplete from './pages/AssessmentComplete';

const queryClient = new QueryClient();

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<Home />} />
            <Route path="/assessment/step-1" element={<Step1PersonalRisk />} />
            <Route path="/assessment/step-2" element={<Step2IncomeExpenses />} />
            <Route path="/assessment/step-3" element={<Step3AssetsLiabilities />} />
            <Route path="/assessment/step-4" element={<Step4FinancialGoals />} />
            <Route path="/assessment/step-5" element={<Step5InsuranceGap />} />
            <Route path="/assessment/step-6" element={<Step6TaxOptimization />} />
            <Route path="/assessment/complete" element={<AssessmentComplete />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
