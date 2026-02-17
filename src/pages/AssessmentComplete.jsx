import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, ArrowRight } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';

const AssessmentComplete = () => {
    const navigate = useNavigate();
    const { completeAssessment } = useAssessmentStore();

    useEffect(() => {
        completeAssessment();
    }, []);

    return (
        <div className="flex flex-col items-center justify-center h-full p-6 text-center">
            <div className="w-24 h-24 bg-green-500/10 rounded-full flex items-center justify-center text-green-500 mb-6 animate-bounce">
                <CheckCircle className="w-12 h-12" />
            </div>
            <h1 className="text-2xl font-bold text-slate-900 mb-2">Assessment Complete!</h1>
            <p className="text-slate-500 mb-8">
                We have analyzed your financial profile. Your personalized financial plan is ready.
            </p>

            <button
                onClick={() => navigate('/')}
                className="w-full bg-slate-900 text-white font-bold py-4 rounded-xl flex items-center justify-center gap-2 hover:bg-slate-800 transition-colors shadow-lg"
            >
                View Dashboard
                <ArrowRight className="w-5 h-5" />
            </button>
        </div>
    );
};

export default AssessmentComplete;
