function SubmitButton({ pending, disabled, pendingLabel, children}) {
    return (
        <button
            type="submit"
            disabled={disabled || pending}>
                {pending? pendingLabel : children}
            </button>
    )
}

export default SubmitButton