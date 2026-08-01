import "jsr:@supabase/functions-js/edge-runtime.d.ts"
import { withSupabase } from "jsr:@supabase/server@^1"
import { GoogleAuth } from "npm:google-auth-library@9"

type PushRequest = {
  expense_id: string
  household_id: string
}

export default {
  fetch: withSupabase({ auth: "user" }, async (req, ctx) => {
    try {
      // Supabase clients may send the JSON body directly or under `data`.
      // Supporting both keeps Android and dashboard invocations compatible.
      const raw = await req.json()
      const input = (raw?.data ?? raw?.body ?? raw) as PushRequest
      console.info("push payload keys", Object.keys(raw ?? {}))
      // `withSupabase` exposes a normalized user identity. Its UUID is `id`;
      // the equivalent raw JWT value would be available as `jwtClaims.sub`.
      const userId = ctx.userClaims?.id
      if (!userId || !input.expense_id || !input.household_id) {
        return Response.json({ error: "Nieprawidłowe dane" }, { status: 400 })
      }

      // The caller must be a member of this household. ctx.supabase obeys RLS.
      const { data: membership } = await ctx.supabase
        .from("razem_members")
        .select("user_id")
        .eq("household_id", input.household_id)
        .eq("user_id", userId)
        .maybeSingle()
      if (!membership) return Response.json({ error: "Brak dostępu" }, { status: 403 })

      // Admin client is server-only; it selects tokens of the other household member.
      const { data: targets, error: targetsError } = await ctx.supabaseAdmin
        .from("razem_device_tokens")
        .select("token")
        .eq("household_id", input.household_id)
        .neq("user_id", userId)
      if (targetsError) {
        console.error(
          "device token query failed",
          targetsError.code,
          targetsError.message,
          targetsError.details,
        )
        throw new Error("Nie udało się pobrać tokenów urządzeń")
      }
      if (!targets?.length) return Response.json({ sent: 0 })

      const credentials = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")!)
      const auth = new GoogleAuth({
        credentials,
        scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
      })
      const accessToken = await auth.getAccessToken()
      if (!accessToken) throw new Error("Nie udało się uzyskać tokenu Firebase")
      const endpoint = `https://fcm.googleapis.com/v1/projects/${credentials.project_id}/messages:send`

      let sent = 0
      for (const target of targets) {
        const response = await fetch(endpoint, {
          method: "POST",
          headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
          body: JSON.stringify({
            message: {
              token: target.token,
              data: {
                expense_id: String(input.expense_id),
              },
              android: { priority: "high" },
            },
          }),
        })
        if (response.ok) {
          sent++
        } else {
          // FCM error bodies identify configuration and stale-token failures;
          // they do not contain the device token sent in the request.
          const responseBody = await response.text()
          console.error(
            "FCM rejected a push request",
            response.status,
            responseBody,
          )
          if (response.status === 404) {
            const { error: deleteError } = await ctx.supabaseAdmin
              .from("razem_device_tokens")
              .delete()
              .eq("token", target.token)
            if (deleteError) console.error("stale token cleanup failed", deleteError.message)
          }
        }
      }
      if (sent === 0 && targets.length > 0) {
        throw new Error("Firebase odrzucił wszystkie powiadomienia")
      }
      return Response.json({ sent })
    } catch (error) {
      // Log only the diagnostic message. Never log credentials or device tokens.
      const message = error instanceof Error
        ? error.message
        : typeof error === "object" && error !== null && "message" in error
          ? String(error.message)
          : String(error)
      console.error("send-expense-push failed", message)
      return Response.json({ error: "Nie udało się wysłać powiadomienia" }, { status: 500 })
    }
  }),
}
