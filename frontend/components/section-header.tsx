import { cn } from "@/lib/utils"
import { ChevronRight } from "lucide-react"

interface SectionHeaderProps {
  title: string
  subtitle?: string
  action?: {
    label: string
    onClick?: () => void
  }
  className?: string
}

export function SectionHeader({ 
  title, 
  subtitle, 
  action,
  className 
}: SectionHeaderProps) {
  return (
    <div className={cn("flex items-center justify-between", className)}>
      <div className="flex flex-col gap-0.5">
        <h2 className="text-lg font-bold text-foreground">{title}</h2>
        {subtitle && (
          <p className="text-sm text-muted-foreground">{subtitle}</p>
        )}
      </div>
      {action && (
        <button 
          onClick={action.onClick}
          className="flex items-center gap-0.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          {action.label}
          <ChevronRight className="size-4" />
        </button>
      )}
    </div>
  )
}
