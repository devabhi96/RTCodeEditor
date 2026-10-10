import { useWebSocketService } from './WebSocketService.js';

/**
 * Hook for executing code snippets via the backend execution service.
 * Provides functions to send execution requests and subscribe to execution output.
 *
 * @returns {Object} Object with executeCode and subscribeToExecutionOutput functions.
 */
export const useExecutionService = () => {
  const { sendMessage, subscribeToTopic } = useWebSocketService();

  /**
   * Sends an execution request to the backend.
   *
   * @param {string} documentId - The ID of the document room.
   * @param {string} language - The programming language (e.g., 'java', 'python', 'nodejs').
   * @param {string} code - The source code to execute.
   * @returns {boolean} True if the message was sent successfully, false otherwise.
   */
  const executeCode = (documentId, language, code) => {
    return sendMessage('/app/execute', {
      documentId,
      language,
      code
    });
  };

  /**
   * Subscribes to execution output for a given document ID.
   * The callback will be invoked with an execution response object.
   *
   * @param {string} documentId - The ID of the document room.
   * @param {Function} callback - Function to call with the execution response.
   * @returns {Object} Subscription object (to be unsubscribed later).
   */
  const subscribeToExecutionOutput = (documentId, callback) => {
    return subscribeToTopic('/user/queue/execution/output', (message) => {
      // Filter messages by documentId to ensure we only process output for this room.
      if (message.documentId === documentId) {
        callback(message);
      }
    });
  };

  return { executeCode, subscribeToExecutionOutput };
};

export { useExecutionService };