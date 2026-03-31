import { useState, useEffect } from 'react';

const SectionNav = ({ sections }) => {
    const [activeId, setActiveId] = useState(sections[0]?.id);

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                const visible = entries
                    .filter((e) => e.isIntersecting)
                    .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
                if (visible.length > 0) {
                    setActiveId(visible[0].target.id);
                }
            },
            { rootMargin: '-20% 0px -60% 0px', threshold: 0 }
        );

        sections.forEach(({ id }) => {
            const el = document.getElementById(id);
            if (el) observer.observe(el);
        });

        return () => observer.disconnect();
    }, [sections]);

    const scrollTo = (id) => {
        const el = document.getElementById(id);
        if (!el) return;
        const header = document.querySelector('header');
        const offset = header ? header.getBoundingClientRect().height + 16 : 80;
        const top = el.getBoundingClientRect().top + window.scrollY - offset;
        window.scrollTo({ top, behavior: 'smooth' });
    };

    return (
        <nav className="fixed right-4 top-1/2 -translate-y-1/2 z-40 hidden lg:flex flex-col items-end gap-0">
            {sections.map(({ id, label }, i) => {
                const isActive = activeId === id;
                const isPast = sections.findIndex((s) => s.id === activeId) > i;
                return (
                    <div key={id} className="flex flex-col items-end">
                        {/* Dot + Label row */}
                        <button
                            onClick={() => scrollTo(id)}
                            className="flex items-center gap-2.5 group cursor-pointer py-1"
                        >
                            <span
                                className={`text-[11px] font-medium tracking-wide transition-all duration-300 ${
                                    isActive
                                        ? 'text-primary opacity-100'
                                        : 'text-slate-500 opacity-0 group-hover:opacity-100'
                                }`}
                            >
                                {label}
                            </span>
                            <span
                                className={`rounded-full transition-all duration-300 shrink-0 ${
                                    isActive
                                        ? 'w-3.5 h-3.5 bg-primary shadow-[0_0_10px_rgba(10,184,66,0.5)] ring-2 ring-primary/20'
                                        : isPast
                                            ? 'w-2.5 h-2.5 bg-primary/40 group-hover:bg-primary/60'
                                            : 'w-2.5 h-2.5 bg-slate-600 group-hover:bg-slate-400'
                                }`}
                            />
                        </button>

                        {/* Connecting line */}
                        {i < sections.length - 1 && (
                            <div className="flex justify-end w-full pr-[5px]">
                                <div
                                    className={`w-px h-4 transition-colors duration-300 ${
                                        isPast ? 'bg-primary/30' : 'bg-white/8'
                                    }`}
                                />
                            </div>
                        )}
                    </div>
                );
            })}
        </nav>
    );
};

export default SectionNav;
