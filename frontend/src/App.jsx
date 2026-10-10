import React, { useEffect, useState } from 'react'
import EditorComponent from './components/EditorComponent'
import { isValidDocumentId } from './services/documentRoom'
import './App.css'

const DEFAULT_DOCUMENT_ID = 'default'

const readDocumentIdFromUrl = () => {
  const documentId = new URLSearchParams(window.location.search).get('documentId')
  return isValidDocumentId(documentId) ? documentId : DEFAULT_DOCUMENT_ID
}

function App() {
  const [documentId, setDocumentId] = useState(readDocumentIdFromUrl)
  const [roomDraft, setRoomDraft] = useState(readDocumentIdFromUrl)
  const [roomError, setRoomError] = useState('')
  const [copyStatus, setCopyStatus] = useState('')

  useEffect(() => {
    const url = new URL(window.location.href)
    url.searchParams.set('documentId', documentId)
    window.history.replaceState(null, '', url)

    const handlePopState = () => {
      const nextDocumentId = readDocumentIdFromUrl()
      setDocumentId(nextDocumentId)
      setRoomDraft(nextDocumentId)
      setRoomError('')
    }
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  const joinRoom = (event) => {
    event.preventDefault()
    const nextDocumentId = roomDraft.trim()
    if (!isValidDocumentId(nextDocumentId)) {
      setRoomError('Use 1–128 letters, numbers, hyphens, or underscores.')
      return
    }

    const url = new URL(window.location.href)
    url.searchParams.set('documentId', nextDocumentId)
    window.history.pushState(null, '', url)
    setDocumentId(nextDocumentId)
    setRoomDraft(nextDocumentId)
    setRoomError('')
    setCopyStatus('')
  }

  const createRoom = () => {
    if (!globalThis.crypto?.randomUUID) {
      setRoomError('Room creation needs a browser with crypto.randomUUID support.')
      return
    }
    const nextDocumentId = globalThis.crypto.randomUUID()
    setRoomDraft(nextDocumentId)
    setRoomError('')
    const url = new URL(window.location.href)
    url.searchParams.set('documentId', nextDocumentId)
    window.history.pushState(null, '', url)
    setDocumentId(nextDocumentId)
    setCopyStatus('')
  }

  const copyInviteLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setCopyStatus('Invite link copied')
    } catch {
      setCopyStatus('Copy is unavailable; share the URL from your address bar')
    }
  }

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">Collaborative workspace</p>
          <h1>Real-Time Code Editor</h1>
          <p className="app-subtitle">Edit together in a shared document room.</p>
        </div>
        <span className="phase-badge">Live collaboration</span>
      </header>

      <section className="room-toolbar" aria-label="Document room controls">
        <form className="room-form" onSubmit={joinRoom}>
          <label htmlFor="document-room">Room ID</label>
          <div className="room-form-row">
            <input
              id="document-room"
              value={roomDraft}
              onChange={(event) => {
                setRoomDraft(event.target.value)
                setRoomError('')
              }}
              maxLength={128}
              autoComplete="off"
              spellCheck="false"
              aria-describedby={roomError ? 'room-error' : 'room-hint'}
            />
            <button type="submit" className="button button-primary">Join room</button>
            <button type="button" className="button button-secondary" onClick={createRoom}>Create room</button>
          </div>
          {roomError
            ? <p id="room-error" className="room-message room-error" role="alert">{roomError}</p>
            : <p id="room-hint" className="room-message">Room IDs can contain letters, numbers, hyphens, and underscores.</p>}
        </form>
        <div className="active-room">
          <span className="active-room-label">Current room</span>
          <code>{documentId}</code>
          <button type="button" className="button button-link" onClick={copyInviteLink}>Copy invite link</button>
          {copyStatus && <span className="copy-status" role="status">{copyStatus}</span>}
        </div>
      </section>

      <EditorComponent documentId={documentId} />
    </main>
  )
}

export default App
