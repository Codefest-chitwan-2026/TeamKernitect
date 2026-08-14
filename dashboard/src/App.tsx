import { useState } from 'react'
import './App.css'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <div className="min-h-screen bg-slate-900 flex items-center justify-center">
        <h1 className="text-4xl font-bold text-white">
          SAHARA
        </h1>
      </div>
    </>
  )
}

export default App
