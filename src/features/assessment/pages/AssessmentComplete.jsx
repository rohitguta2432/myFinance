import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, CheckCircle2, Sparkles, Trophy } from 'lucide-react';
import { useAssessmentStore } from '../store/useAssessmentStore';

const AssessmentComplete = () => {
    const navigate = useNavigate();
    const { completeAssessment } = useAssessmentStore();
    const [showContent, setShowContent] = useState(false);

    useEffect(() => {
        completeAssessment();
        // Staggered animation effect
        const timer = setTimeout(() => setShowContent(true), 300);
        return () => clearTimeout(timer);
    }, [completeAssessment]);

    const checklist = [
        "Net Worth Calculated",
        "Insurance Gaps Identified",
        "Tax Savings Found",
        "Goals Mapped"
    ];

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-background-dark p-6 relative overflow-hidden">
            {/* Confetti / Background Effects */}
            <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-primary/20 rounded-full blur-[100px] animate-pulse"></div>
            <div className="absolute bottom-[-10%] right-[-10%] w-96 h-96 bg-blue-500/20 rounded-full blur-[100px] animate-pulse" style={{ animationDelay: '1s' }}></div>

            <div className={`max-w-md w-full bg-surface-dark border border-white/10 rounded-3xl p-8 relative z-10 shadow-2xl transition-all duration-1000 transform ${showContent ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-10'}`}>

                {/* Icon & Ribbon */}
                <div className="flex justify-center mb-6">
                    <div className="relative">
                        <div className="absolute -inset-4 bg-primary/20 rounded-full blur-lg animate-pulse"></div>
                        <div className="w-20 h-20 bg-gradient-to-br from-primary to-emerald-600 rounded-full flex items-center justify-center shadow-lg relative z-10 border-4 border-surface-dark">
                            <Trophy className="w-10 h-10 text-background-dark" />
                        </div>
                        <Sparkles className="absolute -top-2 -right-2 text-yellow-400 w-6 h-6 animate-bounce" />
                    </div>
                </div>

                <div className="text-center mb-8">
                    <div className="inline-block bg-primary/10 text-primary border border-primary/20 px-3 py-1 rounded-full text-xs font-bold tracking-widest uppercase mb-4">
                        🎉 Assessment Complete!
                    </div>
                    <h1 className="text-2xl sm:text-3xl font-bold text-white mb-3">
                        Your Financial Position Summary is ready!
                    </h1>
                    <p className="text-slate-400 text-sm">
                        {"We've crunched the numbers and built your personalized roadmap."}
                    </p>
                </div>

                {/* Checklist */}
                <div className="bg-background-dark/50 border border-white/5 rounded-2xl p-5 mb-8 space-y-4">
                    {checklist.map((item, index) => (
                        <div
                            key={index}
                            className={`flex items-center gap-3 transition-all duration-700 transform ${showContent ? 'opacity-100 translate-x-0' : 'opacity-0 -translate-x-4'}`}
                            style={{ transitionDelay: `${400 + index * 150}ms` }}
                        >
                            <div className="bg-primary/20 p-1 rounded-full">
                                <CheckCircle2 className="w-4 h-4 text-primary" />
                            </div>
                            <span className="text-white font-medium text-sm">{item}</span>
                        </div>
                    ))}
                </div>

                {/* Call to Action */}
                <button
                    onClick={() => navigate('/dashboard')}
                    className={`w-full bg-primary hover:bg-primary-dark active:scale-[0.98] transition-all text-background-dark font-bold text-lg py-4 rounded-xl flex items-center justify-center gap-2 shadow-[0_0_20px_rgba(13,242,89,0.3)] duration-1000 ${showContent ? 'opacity-100 scale-100' : 'opacity-0 scale-95'}`}
                    style={{ transitionDelay: '1000ms' }}
                >
                    View Your Dashboard
                    <ArrowRight className="w-5 h-5" />
                </button>
            </div>
        </div>
    );
};

export default AssessmentComplete;
