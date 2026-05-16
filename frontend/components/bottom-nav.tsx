"use client"

import { usePathname, useRouter } from "next/navigation"
import { Home, Heart, CalendarDays, BookHeart, User } from "lucide-react"
import { cn } from "@/lib/utils"

interface NavItemProps {
  icon: React.ReactNode
  label: string
  href: string
  isActive?: boolean
}

function NavItem({ icon, label, href, isActive = false }: NavItemProps) {
  const router = useRouter()
  
  return (
    <button 
      onClick={() => router.push(href)}
      className={cn(
        "flex flex-col items-center gap-0.5 py-2 px-3 rounded-xl transition-colors",
        isActive 
          ? "text-primary" 
          : "text-muted-foreground hover:text-foreground"
      )}
    >
      <div className={cn(
        "p-1.5 rounded-xl transition-colors",
        isActive && "bg-primary/10"
      )}>
        {icon}
      </div>
      <span className="text-[11px] font-medium">{label}</span>
    </button>
  )
}

const navItems = [
  { icon: <Home className="size-5" />, label: "홈", href: "/" },
  { icon: <Heart className="size-5" />, label: "만남", href: "/slots" },
  { icon: <CalendarDays className="size-5" />, label: "플래너", href: "/planner" },
  { icon: <BookHeart className="size-5" />, label: "일기", href: "/diary" },
  { icon: <User className="size-5" />, label: "MY", href: "/profile" },
]

export function BottomNav() {
  const pathname = usePathname()

  return (
    <nav className={cn(
      "absolute bottom-0 left-0 right-0 z-50",
      "bg-card/95 backdrop-blur-sm border-t border-border/50",
      "px-2 pb-safe"
    )}>
      <div className="flex items-center justify-around">
        {navItems.map((item) => (
          <NavItem
            key={item.href}
            icon={item.icon}
            label={item.label}
            href={item.href}
            isActive={pathname === item.href}
          />
        ))}
      </div>
    </nav>
  )
}
