import React, { useEffect, useState, useRef } from 'react';
import { useExecutionService } from '../services/executionService.js';
import './ExecutionConsole.css';

/**
 * Component for executing code snippets in the current document room.
 * Displays language selector, run button, and output console.
 *
 * @param {Object} props
 * @param {string} props.documentId - The ID of the current document room.
 * @param {string} props.currentCode - The current code in the editor.
 * @param {Function} props.setCurrentCode - Optional: setter to update the editor code (not used).
 */
export const ExecutionConsole = ({ documentId, currentCode }) => {
  const [language, setLanguage] = useState('java'); // default to Java
  const [output, setOutput] = useState({ stdout: '', stderr: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [executed, setExecuted] = useState(false);
  const { executeCode, subscribeToExecutionOutput } = useExecutionService();
  const subscriptionRef = useRef(null);

  // Subscribe to execution output for this document when the component mounts or documentId changes.
  useEffect(() => {
    if (!documentId) return;
    const subscription = subscribeToExecutionOutput(documentId, (response) => {
      // Update output with the execution result.
      setOutput({
        stdout: response.stdout || '',
        stderr: response.stderr || ''
      });
      setLoading(false);
      setError(null);
      setExecuted(true);
    });
    subscriptionRef.current = subscription;

    // Cleanup subscription on unmount or when documentId changes.
    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
      }
    };
  }, [documentId, subscribeToExecutionOutput]);

  const handleRun = () => {
    if (!currentCode || currentCode.trim() === '') {
      setError('Code is empty');
      return;
    }
    setLoading(true);
    setError(null);
    setOutput({ stdout: '', stderr: '' });
    setExecuted(false);
    // Send execution request to backend.
    const sent = executeCode(documentId, language, currentCode);
    if (!sent) {
      setLoading(false);
      setError('Failed to send execution request (WebSocket not connected)');
    }
  };

  const handleClear = () => {
    setOutput({ stdout: '', stderr: '' });
    setError(null);
    setLoading(false);
    setExecuted(false);
  };

  return (
    <div className="execution-console">
      <div className="execution-header">
        <h3>Execution Console</h3>
        <div className="execution-controls">
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="language-select"
            disabled={loading}
          >
            <option value="java">Java</option>
            <option value="python">Python</option>
            <option value="nodejs">JavaScript (Node.js)</option>
          </select>
          <button
            onClick={handleRun}
            disabled={loading || !currentCode || currentCode.trim() === ''}
            className={loading ? 'button button-loading' : 'button button-primary'}
          >
            {loading ? 'Running...' : 'Run'}
          </button>
          <button
            onClick={handleClear}
            disabled={loading}
            className="button button-secondary"
          >
            Clear
          </button>
        </div>
      </div>
      <div className="execution-output">
        {loading && (
          <div className="execution-loading">
            Executing code...
          </div>
        )}
        {!loading && executed && (
          <>
            {output.stdout && (
              <div className="execution-section">
                <h4>Stdout:</h4>
                <pre className="execution-stdout">{output.stdout}</pre>
              </div>
            )}
            {output.stderr && (
              <div className="execution-section">
                <h4>Stderr:</h4>
                <pre className="execution-stderr">{output.stderr}</pre>
              </div>
            )}
            {!output.stdout && !output.stderr && (
              <div className="execution-section">
                <h4>Output:</h4>
                <p className="execution-empty">No output.</p>
              </div>
            )}
          </>
        )}
        {error && (
          <div className="execution-error">
            Error: {error}
          </div>
        )}
      </div>
    </div>
  );
};

export default ExecutionConsole;