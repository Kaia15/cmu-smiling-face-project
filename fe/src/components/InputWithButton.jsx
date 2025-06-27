import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export function InputWithButton({topic, setTopic, loading, handleSubmit}) {
  return (
    <div className="flex w-full max-w-md items-center my-4 gap-2">
      <Input type="text" placeholder="Search Topic" 
      value = {topic}
      onChange = {(e) => setTopic(e.target.value)}
      />
      <Button 
      onClick={handleSubmit} 
      disabled={loading} 
      variant="outline">
        Search
      </Button>
    </div>
  )
}
