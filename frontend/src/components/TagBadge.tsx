import { Badge } from '@/components/ui/badge'

interface Props {
  name: string
}

export function TagBadge({ name }: Props) {
  return (
    <Badge
      variant="outline"
      className="border-primary/40 text-muted-foreground hover:border-primary hover:text-foreground transition-colors"
    >
      {name}
    </Badge>
  )
}
