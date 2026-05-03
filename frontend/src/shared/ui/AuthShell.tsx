import type { ReactNode } from 'react'

type AuthShellProps = {
  title: string
  children: ReactNode
}

export default function AuthShell({ title, children }: AuthShellProps) {
  return (
    <main className="min-h-screen bg-slate-100 px-4 py-12">
      <div className="mx-auto w-full max-w-md rounded-lg border border-slate-200 bg-white p-8 shadow-sm">
        <p className="text-center text-sm font-medium uppercase tracking-wide text-slate-500">
          bozkurt
        </p>
        <h1 className="mt-2 text-center text-2xl font-semibold text-slate-900">
          {title}
        </h1>
        {children}
      </div>
    </main>
  )
}
