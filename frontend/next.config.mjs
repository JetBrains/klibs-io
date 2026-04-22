/** @type {import('next').NextConfig} */
const nextConfig = {
	async rewrites() {
		return [
			{
				source: '/sitemap.xml',
				destination: `${process.env.NEXT_PUBLIC_API_URL}/sitemap.xml`,
			},
		];
	},
	images: {
		remotePatterns: [
			{
				protocol: 'https',
				hostname: 'avatars.githubusercontent.com'
			}
		],
	}
};

export default nextConfig;
