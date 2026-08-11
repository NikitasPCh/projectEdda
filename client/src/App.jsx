import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import SubmitButton from './components/SubmitButton'
import './App.css'

const PASSWORD_RULES = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/

function App() {
  const [view, setView] = useState('checking')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [registerUsername, setRegisterUsername] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [playerId, setPlayerId] = useState(null)
  const passwordValid = registerPassword.length >= 8 && PASSWORD_RULES.test(registerPassword)
  const [autoLoginFailedMessage, setAutoLoginFailedMessage] = useState(null)

  async function fetchCharacter() {
    const response = await fetch('http://localhost:8080/api/players/character', {
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error('Failed to fetch character')
    }
    return response.json()
  }

  async function loginUser({ username, password }) {
    let response
    try {
      response = await fetch('http://localhost:8080/api/players/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ username, password }),
      })
    } catch {
      throw new Error('Could not reach the server')
    }

    if (!response.ok) {
      if (response.status === 401) {
        throw new Error('Incorrect username or password')
      }
      throw new Error('Something went wrong, please try again')
    }

    return response.json()
  }

  async function registerUser({ username, password }) {
    let response
    try {
      response = await fetch('http://localhost:8080/api/players', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({username, password}),
      })
    } catch {
      throw new Error('Could not reach the server')
    }

    if(!response.ok) {
      const data = await response.json()
      throw new Error(data.message)
    }

    return response.json()
  }

  async function logoutUser() {
    let response
    try {
      response = await fetch('http://localhost:8080/api/players/logout', {
        method: 'POST',
        credentials: 'include',
      })
    } catch {
      throw new Error('Could not reach the server')
    }

    if (!response.ok) {
      throw new Error('Something went wrong, please try again')
    }
  }

  const loginMutation = useMutation({
    mutationFn: loginUser,
    onSuccess: (data) => {
      setPlayerId(data.playerId)
      setView('dashboard')
    },
  })

  const registerMutation = useMutation({
    mutationFn: registerUser,
    onSuccess: () => {
      loginMutation.mutate(
        { username: registerUsername, password:registerPassword },
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
    onSuccess: () => {
      setPlayerId(null)
      setUsername('')
      setPassword('')
      setView('login')
    },
  })

  const { data: character, isLoading, isSuccess, isError } = useQuery({
    queryKey: ['character', playerId],
    queryFn: fetchCharacter,
    retry: false,
  })

  useEffect(() => {
    if (isSuccess) {
      setView('dashboard')
    } else if (isError) {
      setView('login')
    }
  }, [isSuccess, isError])

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
            registerMutation.mutate({ username:registerUsername, password:registerPassword })
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
    </div>
  )
}

export default App