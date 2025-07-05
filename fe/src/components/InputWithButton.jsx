import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

export function InputWithButton({topics, input, setInput, loading, handleSubmit, handleEnter, handleClear}) {
  return (
    <div className="flex w-1/2 items-center my-4 flex flex-col">
      <Input type="text" placeholder="Search Topic" 
      value = {input}
      onChange = {(e) => setInput(e.target.value)}
      onKeyDown = {handleEnter}
      className = "shadow-none border-0 border-b-1"
      />
      <div className="flex flex-row w-full my-2">
        <p className = "mr-2"> Keywords</p>
        <div className="flex flex-row">
          {topics?.map((topic, topicId) => {
          return (
            <div
              key={topicId}
              className="inline-flex items-center bg-blue-100 text-blue-800 border border-blue-300 rounded-lg px-2 py-1 mr-2 mb-2 shadow-sm"
            >
              <span className="mr-2">{topic}</span>
              <button
                onClick={() => handleClear(topicId)}
                className="text-blue-500 hover:text-blue-700 hover:bg-blue-200 rounded-full w-5 h-5 flex items-center justify-center transition"
                aria-label={`Remove ${topic}`}
              >
                ×
              </button>
            </div>

          )
        })}
        </div>
        <div className="justify-end">
          <Button 
          onClick={handleSubmit} 
          disabled={loading} 
          variant="">
            Search
          </Button>
        </div>
      </div>
    </div>
  )
}
