import { useLayoutEffect, useRef, useState } from 'react';

const DEFAULT_PAGE_SIZE = 10;
const BOTTOM_SPACING = 24;


export function useFitPageSize(deps = [], minRows = 3) {
  const containerRef = useRef(null);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);

  useLayoutEffect(() => {
    const calc = () => {
      const el = containerRef.current;
      if (!el) return;
      const thead = el.querySelector('thead');
      const row = el.querySelector('tbody tr');
      const foot = el.querySelector('.table-foot');
      if (!thead || !row || !foot) return;

      const rowHeight = row.getBoundingClientRect().height;
      const theadHeight = thead.getBoundingClientRect().height;
      const footHeight = foot.getBoundingClientRect().height;
      const top = el.getBoundingClientRect().top;

      const available = window.innerHeight - top - theadHeight - footHeight - BOTTOM_SPACING;
      const rows = Math.floor(available / rowHeight);
      setPageSize(Math.max(minRows, rows));
    };

    calc();
    window.addEventListener('resize', calc);
    return () => window.removeEventListener('resize', calc);
  }, deps);

  return [containerRef, pageSize];
}
