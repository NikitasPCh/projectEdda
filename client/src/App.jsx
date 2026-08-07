import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import './App.css'

const PASSWORD_RULES = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).+$/

function App() {
  const [view, setView] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [registerUsername, setRegisterUsername] = useState('')
  const [registerPassword, setRegisterPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [playerId, setPlayerId] = useState(null)
  const passwordValid = registerPassword.length >= 8 && PASSWORD_RULES.test(registerPassword)

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

  const loginMutation = useMutation({
    mutationFn: loginUser,
    onSuccess: (data) => {
      setPlayerId(data.playerId)
      setView('dashboard')
    },
  })

  const { data: character, isLoading, isError } = useQuery({
    queryKey: ['character', playerId],
    queryFn: fetchCharacter,
    enabled: view === 'dashboard',
  })

  return (
    <div>
      <h1>Edda</h1>

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
          {loginMutation.isError && <p>{loginMutation.error.message}</p>}
          <button type="submit" disabled={!username.trim() || !password.trim()}>
            Log in
          </button>
          <p>
            Don't have an account?{' '}
            <button type="button" onClick={() => setView('register')}>
              Register
            </button>
          </p>
        </form>
      )}

      {view === 'register' && (
        <form noValidate>
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
          <button
            type="submit"
            disabled={
              !registerUsername.trim() ||
              !passwordValid ||
              registerPassword !== confirmPassword
            }
          >
            Register
          </button>
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
          <h2>Logged in as {username}</h2>
          {isLoading && <p>Loading character...</p>}
          {isError && <p>Could not load character data.</p>}
          {character && (
            <div>
              <h3>{character.name}</h3>

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