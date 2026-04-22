import { cookies } from 'next/headers'
import { NextResponse } from 'next/server'

export async function POST() {
  const cookieStore = await cookies()
  cookieStore.set({
    name: 'notificationHidden',
    value: '1',
    httpOnly: true,
    path: '/',
  })
  return new NextResponse(null, { status: 204 })
}
