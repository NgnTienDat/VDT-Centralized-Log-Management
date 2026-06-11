import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'
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
