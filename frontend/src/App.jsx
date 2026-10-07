import React from 'react'
import EditorComponent from './components/EditorComponent'

function App() {
  return (
    <main className="App">
      <h1>Real-Time Code Editor</h1>
      <EditorComponent documentId="default" />
    </main>
  )
}

export default App