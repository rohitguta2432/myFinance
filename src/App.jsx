import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/layout/Layout';
import Home from './features/dashboard/pages/Home';
import Step1PersonalRisk from './features/assessment/pages/Step1PersonalRisk';
import Step2IncomeExpenses from './features/assessment/pages/Step2IncomeExpenses';
import Step3AssetsLiabilities from './features/assessment/pages/Step3AssetsLiabilities';
import Step4FinancialGoals from './features/assessment/pages/Step4FinancialGoals';
import Step5InsuranceGap from './features/assessment/pages/Step5InsuranceGap';
import Step6TaxOptimization from './features/assessment/pages/Step6TaxOptimization';
import AssessmentComplete from './features/assessment/pages/AssessmentComplete';

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
