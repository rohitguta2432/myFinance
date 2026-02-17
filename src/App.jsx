import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Layout from './components/Layout';
import Home from './pages/Home';

const queryClient = new QueryClient();

const Transactions = () => (
  <div className="p-6 space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
    <h1 className="text-2xl font-bold text-slate-900">Activity</h1>
    <div className="p-8 text-center bg-white rounded-2xl border border-slate-100 shadow-sm">
      <p className="text-slate-500">No transactions yet.</p>
    </div>
  </div>
);

const Profile = () => (
  <div className="p-6 space-y-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
    <h1 className="text-2xl font-bold text-slate-900">Profile</h1>
    <div className="p-4 bg-white rounded-2xl border border-slate-100 shadow-sm flex items-center gap-4">
      <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 text-xl font-bold">RR</div>
      <div>
        <h2 className="font-bold text-slate-900">Rohit Raj</h2>
        <p className="text-sm text-slate-500">rohit@example.com</p>
      </div>
    </div>
  </div>
);

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<Home />} />
            <Route path="/transactions" element={<Transactions />} />
            <Route path="/profile" element={<Profile />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

