import { useState } from 'react'
import LogDashboard from './pages/LogDashboard'

function App() {
  const [count, setCount] = useState(0)

  return (
    <>
      <LogDashboard />
    </>
  )
}

export default App
