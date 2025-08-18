import React from 'react'
import Link from 'next/link'
import Image from 'next/image'
//import { signOut } from 'next-auth/react'; // Import signOut function from next-auth

const Navbar = () => {
    //conast session = await auth(); we can use this to validate wether user is logged in 
    // or not and show the items on navbar as needed
    //also need to convert const Navbar = async () => {} to use await.
  return (
    <header className="px-5 py-3 bg-white shadow-sm">
        <nav className="flex justify-between items-center">
            <Link href="/">
                <Image src="/logo.png" alt="Logo" width={144} height={30} />           
            </Link>
            <div className="flex items-center gap-5 text-black">
                <form action={async () =>{
                    "use server";
                    //await signOut(Option, { redirectTo : "/" }) //signOut function to log out the user;
                }}>
                    <button type="submit">
                        <Link href="/auth/login">LogIn</Link>
                    </button>
                </form>
                {/* comment out the below code to make the visible/non visible elements on navbar if the user is 
                logged in or admin is logged in. */}

                {/* {session && sessionStorage.?user ? (
                    <>
                          <Link href="/AI">AI</Link>
                          <button onClick={signout}>
                              <span>Log Out</span>
                          </button><Link href={`/user/${session?.id}`}>
                              <span>{session?.user?.name}</span>
                          </Link>
                    </>
                )} : (
                    <>
                        <Link href="/login">Login</Link>
                        <Link href="/register">Register</Link>
                    </>
                )} */}
                
            </div>
        </nav>
    </header>
  )
}

export default Navbar