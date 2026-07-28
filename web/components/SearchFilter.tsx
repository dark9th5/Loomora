'use client';

import React from 'react';
import { Search } from 'lucide-react';

type SearchFilterProps = {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  filters?: Array<{ label: string; value: string; options: Array<{ label: string; value: string }> }>;
  filterValues?: Record<string, string>;
  onFilterChange?: (key: string, value: string) => void;
};

export function SearchFilter({
  value,
  onChange,
  placeholder = 'Search...',
  filters,
  filterValues,
  onFilterChange,
}: SearchFilterProps) {
  return (
    <div className="flex flex-col sm:flex-row gap-3">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className="w-full rounded-lg border border-slate-700 bg-slate-800 py-2 pl-10 pr-4 text-sm text-white placeholder-slate-500 focus:border-loomora-primary focus:outline-none"
        />
      </div>
      {filters?.map((filter) => (
        <select
          key={filter.value}
          value={filterValues?.[filter.value] ?? ''}
          onChange={(e) => onFilterChange?.(filter.value, e.target.value)}
          className="rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-300 focus:border-loomora-primary focus:outline-none"
          aria-label={filter.label}
        >
          <option value="">{filter.label}</option>
          {filter.options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ))}
    </div>
  );
}
