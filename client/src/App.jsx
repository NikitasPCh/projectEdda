import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import SubmitButton from './components/SubmitButton'
import './App.css'

const PASSWORD_RULES = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/
const API_BASE = 'http://localhost:8080/api'

function mergeProgress(character, progress) {
  const skills = character.skills.map((skill) => 
    skill.skillKey === progress.skillKey
      ? { ...skill, xp: skill.xp + progress.xpGained }
      : skill
  )

  const resources = character.resources.map((resource) => 
    resource.resourceKey === progress.resourceKey
    ? { ...resource, quantity: resource.quantity + progress.quantityGained }
    : resource
  )

  let items = character.items
  for (const gained of progress.itemsGained) {
    const alreadyOwned = items.some((item) => item.itemKey === gained.itemKey)
    items = alreadyOwned
      ? items.map((item) =>
        item.itemKey === gained.itemKey
          ? { ...item, quantity: item.quantity + gained.quantityGained }
          : item
      )
    : [...items, { itemKey: gained.itemKey, itemName: gained.itemName, rarity: gained.rarity, quantity: gained.quantityGained }]
  }

  return { ...character, skills, resources, items }
}

function buildTickMessage(progress) {
  let message = `Action completed! Gained ${progress.xpGained} XP and ${progress.quantityGained} ${progress.resourceName}`
  if (progress.itemsGained.length > 0){
    const items = progress.itemsGained
      .map((item) => `${item.quantityGained}x ${item.itemName}`)
      .join(`, `)
    message += `, plus ${items}`
  }
  return message + '.'
}

function App() {
  const [view, setView] = useState('checking')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [registerUsername, setRegisterUsername] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [playerId, setPlayerId] = useState(null)
  const [autoLoginFailedMessage, setAutoLoginFailedMessage] = useState(null)
  const [welcomeProgress, setWelcomeProgress] = useState(null)
  const [justLoggedIn, setJustLoggedIn] = useState(false)
  const [forcedLogoutMessage, setForcedLogoutMessage] = useState(null)
  const [tickMessage, setTickMessage] = useState(() => sessionStorage.getItem('tickMessage'))
  const [tickCount, setTickCount] = useState(0)
  const [initialDelay, setInitialDelay] = useState(0)
  const passwordValid = registerPassword.length >= 8 && PASSWORD_RULES.test(registerPassword)
  const queryClient = useQueryClient()

  async function safeFetch(url, options) {
    try {
      return await fetch(url, options)
    } catch {
      throw new Error('Could not reach the server')
    }
  }

  async function fetchCharacter() {
    const response = await safeFetch(`${API_BASE}/players/character`, {
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error('Failed to fetch character')
    }
    return response.json()
  }

  async function loginUser({ username, password }) {
    const response = await safeFetch(`${API_BASE}/players/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ username, password }),
    })

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('Incorrect username or password')
      }
      throw new Error('Something went wrong, please try again')
    }

    return response.json()
  }

  async function registerUser({ username, password }) {
    const response = await safeFetch(`${API_BASE}/players`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })

    if (!response.ok) {
      const data = await response.json()
      throw new Error(data.message)
    }

    return response.json()
  }

  async function logoutUser() {
    const response = await safeFetch(`${API_BASE}/players/logout`, {
      method: 'POST',
      credentials: 'include',
    })

    if (!response.ok) {
      throw new Error('Something went wrong, please try again')
    }
  }

  const resetToLoggedOut = useCallback(() => {
    queryClient.removeQueries({ queryKey: ['character', playerId] })
    setPlayerId(null)
    setUsername('')
    setPassword('')
    setTickMessage(null)
    sessionStorage.removeItem('tickMessage')
    setView('login')
  }, [playerId, queryClient])

  const loginMutation = useMutation({
    mutationFn: loginUser,
    onSuccess: (data) => {
      setJustLoggedIn(true)
      setPlayerId(data.playerId)
    }
  })

  const registerMutation = useMutation({
    mutationFn: registerUser,
    onSuccess: () => {
      loginMutation.mutate(
        { username: registerUsername, password: registerPassword },
        {
          onError: () => {
            setUsername(registerUsername)
            setAutoLoginFailedMessage(
              'Account created! We could not log you in automatically - please log in below.'
            )
            setView('login')
          }
        }
      )
    }
  })

  const logoutMutation = useMutation({
    mutationFn: logoutUser,
    onSuccess: resetToLoggedOut,
  })

  const { data: character, isLoading, isSuccess, isError } = useQuery({
    queryKey: ['character', playerId],
    queryFn: fetchCharacter,
    retry: false,
  })

  useEffect(() => {
    if (isSuccess) {
      if (justLoggedIn) {
        setWelcomeProgress(character.progress)
        setJustLoggedIn(false)
      }
      setView('dashboard')
    } else if (isError) {
      setJustLoggedIn(false)
      setView('login')
    }
  }, [isSuccess, isError, justLoggedIn, character])

  useEffect(() => {
    if (character?.lastCalculatedAt) {
      setInitialDelay(Math.min((Date.now() - new Date(character.lastCalculatedAt).getTime()) / 1000, 5))
    }
  }, [character?.lastCalculatedAt])

  useEffect(() => {
    if (view !== 'dashboard') {
      return
    }

    const ws = new WebSocket('ws://localhost:8080/api/game')

    ws.onmessage = (event) => {
      const progress = JSON.parse(event.data)
      queryClient.setQueryData(['character', playerId], (old) => mergeProgress(old, progress))
      const message = buildTickMessage(progress)
      setTickMessage(message)
      sessionStorage.setItem('tickMessage', message)
      setTickCount((count) => count + 1)
    }

    ws.onclose = (event) => {
      if (event.code === 4001) {
        resetToLoggedOut()
        setForcedLogoutMessage(event.reason)
      }
    }

    return () => {
      ws.close()
    }
  }, [view, resetToLoggedOut, playerId, queryClient])

  return (
    <div>
      <h1>Edda</h1>
      {view === 'checking' && <p>Checking session...</p>}

      {view === 'login' && (
        <form
          noValidate
          onSubmit={(e) => {
            e.preventDefault()
            loginMutation.mutate({ username, password })
          }}
        >
          <div>
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {autoLoginFailedMessage && <p>{autoLoginFailedMessage}</p>}
          {forcedLogoutMessage && <p>{forcedLogoutMessage}</p>}
          {loginMutation.isError && <p>{loginMutation.error.message}</p>}
          <SubmitButton
            pending={loginMutation.isPending}
            disabled={!username.trim() || !password.trim()}
            pendingLabel="Logging in..."
          >
            Log in
          </SubmitButton>
          <p>
            Don't have an account?{' '}
            <button type="button" onClick={() => setView('register')}>
              Register
            </button>
          </p>
        </form>
      )}

      {view === 'register' && (
        <form
          noValidate
          onSubmit={(e) => {
            e.preventDefault()
            registerMutation.mutate({ username: registerUsername, password: registerPassword })
          }}
        >
          <div>
            <label htmlFor="register-username">Username</label>
            <input
              id="register-username"
              type="text"
              required
              value={registerUsername}
              onChange={(e) => setRegisterUsername(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="register-password">Password</label>
            <input
              id="register-password"
              type="password"
              required
              value={registerPassword}
              onChange={(e) => setRegisterPassword(e.target.value)}
            />
            {registerPassword && !passwordValid && (
              <p>
                Password must be at least 8 characters long and include an uppercase letter, a lowercase letter, a number and a special character.
              </p>
            )}
          </div>
          <div>
            <label htmlFor="confirm-password">Confirm password</label>
            <input
              id="confirm-password"
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
            />
            {confirmPassword && registerPassword !== confirmPassword && (
              <p>Passwords do not match</p>
            )}
          </div>
          {registerMutation.isError && <p>{registerMutation.error.message}</p>}
          <SubmitButton
            pending={registerMutation.isPending}
            disabled={
              !registerUsername.trim() ||
              !passwordValid ||
              registerPassword !== confirmPassword
            }
            pendingLabel="Registering..."
          >
            Register
          </SubmitButton>
          <p>
            Already have an account?{' '}
            <button type="button" onClick={() => setView('login')}>
              Log in
            </button>
          </p>
        </form>
      )}
      {view === 'dashboard' && (
        <div>
          {isLoading && <p>Loading character...</p>}
          {isError && <p>Could not load character data.</p>}
          {character && (
            <div>
              <h3>{character.name}</h3>
              {tickMessage && <p>{tickMessage}</p>}
              {character.currentActionName && (
                <div className="progress-bar-track">
                  <div
                    className="progress-bar-fill"
                    key={tickCount}
                    style={tickCount === 0 ? { animationDelay: `-${initialDelay}s` } : undefined}
                  ></div>
                  <span className="progress-bar-label">{character.currentActionName}</span>
                </div>
              )}
              <button
                type="button"
                onClick={() => logoutMutation.mutate()}
                disabled={logoutMutation.isPending}
              >
                {logoutMutation.isPending ? 'Logging out...' : 'Log Out'}
              </button>
              {logoutMutation.isError && <p>{logoutMutation.error.message}</p>}
              <h4>Skills</h4>
              <ul>
                {character.skills.map((skill) => (
                  <li key={skill.skillKey}>
                    {skill.skillName}: {skill.xp} XP
                  </li>
                ))}
              </ul>

              <h4>Resources</h4>
              <ul>
                {character.resources.map((resource) => (
                  <li key={resource.resourceKey}>
                    {resource.resourceName}: {resource.quantity}
                  </li>
                ))}
              </ul>

              <h4>Items</h4>
              <ul>
                {character.items.map((item) => (
                  <li key={item.itemKey}>
                    {item.itemName} ({item.rarity}): {item.quantity}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
      {welcomeProgress && (
        <div className="modal-overlay">
          <div className="modal">
            <button type="button" className="modal-close" onClick={() => setWelcomeProgress(null)}>
              X
            </button>
            <h3>Welcome back!</h3>
            <p>While away you gained:</p>
            <p>{welcomeProgress.skillName}: +{welcomeProgress.xpGained} XP</p>
            <p>{welcomeProgress.resourceName}: +{welcomeProgress.quantityGained}</p>
            {welcomeProgress.itemsGained.length > 0 && (
              <ul>
                {welcomeProgress.itemsGained.map((item) => (
                  <li key={item.itemKey}>
                    {item.itemName} ({item.rarity}): +{item.quantityGained}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default App