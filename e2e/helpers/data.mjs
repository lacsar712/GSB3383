export function uniqueId(prefix = 'u') {
  const rand = Math.floor(Math.random() * 100000);
  return `${prefix}_${Date.now()}_${rand}`;
}

export function credentials(prefix = 'user') {
  return {
    username: uniqueId(prefix),
    password: 'Pass@12345',
  };
}
