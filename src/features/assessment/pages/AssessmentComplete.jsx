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
        <div className="flex flex-col items-center justify-center h-full p-6 text-center max-w-lg mx-auto">
            <div className="w-24 h-24 bg-primary/10 rounded-full flex items-center justify-center text-primary mb-6 animate-bounce">
                <CheckCircle className="w-12 h-12" />
            </div>
            <h1 className="text-2xl font-bold text-white mb-2">Assessment Complete!</h1>
            <p className="text-slate-400 mb-8">
                We have analyzed your financial profile. Your personalized financial plan is ready.
            </p>

            <button
                onClick={() => navigate('/')}
                className="w-full bg-primary hover:bg-primary-dark active:scale-[0.98] transition-all text-background-dark font-bold py-4 rounded-xl flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(13,242,89,0.3)]"
            >
                View Dashboard
                <ArrowRight className="w-5 h-5" />
            </button>
        </div>
    );
};

export default AssessmentComplete;
