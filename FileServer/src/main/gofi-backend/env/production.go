//go:build production
// +build production

package env

import embed "embed"

const current = Production

var EmbedStaticAssets embed.FS
