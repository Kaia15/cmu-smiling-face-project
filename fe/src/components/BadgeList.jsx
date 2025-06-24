import { AlertCircleIcon, BadgeCheckIcon, CheckIcon } from "lucide-react"

import { Badge } from "@/components/ui/badge"

export function BadgeDemo() {
  return (
    <div className="flex flex-col items-center gap-2">
      <div className="flex w-full flex-wrap gap-2">
        <div className="scroll-m-20 text-base font-semibold tracking-tight">
            Common Topics
        </div>
        <Badge>#Smile</Badge>
        <Badge variant="secondary">#Grace Hopper </Badge>
        <Badge variant="destructive">#Layoffs</Badge>
        <Badge variant="outline">#Google</Badge>
        <Badge
          variant="secondary"
          className="bg-blue-500 text-white dark:bg-blue-600"
        >
          #Tech
        </Badge>
      </div>
      {/* <div className="flex w-full flex-wrap gap-2">
        
        <Badge className="h-5 min-w-5 rounded-full px-1 font-mono tabular-nums">
          8
        </Badge>
        <Badge
          className="h-5 min-w-5 rounded-full px-1 font-mono tabular-nums"
          variant="destructive"
        >
          99
        </Badge>
        <Badge
          className="h-5 min-w-5 rounded-full px-1 font-mono tabular-nums"
          variant="outline"
        >
          20+
        </Badge>
      </div> */}
    </div>
  )
}
