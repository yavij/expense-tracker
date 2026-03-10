/**
 * Wraps fetch with automatic retry logic for transient failures.
 *
 * Retries up to 3 times with exponential backoff (1s, 2s, 4s).
 * Only retries on network errors and 5xx responses, not on 4xx client errors.
 *
 * @param {string} url - The URL to fetch
 * @param {object} options - The fetch options
 * @returns {Promise<Response>} The fetch response
 * @throws {Error} If all retry attempts fail
 */
export async function retryFetch(url, options = {}) {
  const maxRetries = 3;
  const backoffDelays = [1000, 2000, 4000]; // milliseconds: 1s, 2s, 4s

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      const response = await fetch(url, options);

      // Don't retry on 4xx errors (client errors)
      if (response.status >= 400 && response.status < 500) {
        return response;
      }

      // Retry on 5xx errors (server errors) and network errors
      if (response.ok || response.status >= 500) {
        if (response.ok) {
          return response; // Success
        }
        // For 5xx, retry if not the last attempt
        if (attempt < maxRetries) {
          const delay = backoffDelays[attempt];
          await sleep(delay);
          continue;
        }
        return response; // Last attempt, return the 5xx response
      }

      return response;
    } catch (error) {
      // Network error - retry if not the last attempt
      if (attempt < maxRetries) {
        const delay = backoffDelays[attempt];
        await sleep(delay);
        continue;
      }
      // Last attempt failed, throw the error
      throw error;
    }
  }
}

/**
 * Sleep for a specified number of milliseconds.
 *
 * @param {number} ms - The number of milliseconds to sleep
 * @returns {Promise<void>}
 */
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Enhanced version of retryFetch that works with the api() function pattern.
 * Automatically retries the API call if it fails due to network or server errors.
 *
 * @param {Function} apiFn - The API function to retry (e.g., () => api('/endpoint'))
 * @returns {Promise<any>} The result from the API function
 * @throws {Error} If all retry attempts fail
 */
export async function retryApiCall(apiFn) {
  const maxRetries = 3;
  const backoffDelays = [1000, 2000, 4000]; // milliseconds: 1s, 2s, 4s

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await apiFn();
    } catch (error) {
      // Check if this is a network error
      const isNetworkError = error.message.includes('Network error') ||
                            error instanceof TypeError ||
                            !error.message;

      if (isNetworkError && attempt < maxRetries) {
        const delay = backoffDelays[attempt];
        await sleep(delay);
        continue;
      }

      // Last attempt or not a retryable error
      throw error;
    }
  }
}
