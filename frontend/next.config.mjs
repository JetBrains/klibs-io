/** @type {import('next').NextConfig} */
const nextConfig = {
	async rewrites() {
		return [
			{
				source: '/sitemap.xml',
				destination: `${process.env.NEXT_PUBLIC_API_URL}/sitemap.xml`,
			},
			{
				source: '/package/:groupId/:artifactId/:version/status',
				destination: `${process.env.NEXT_PUBLIC_API_URL}/package/:groupId/:artifactId/:version/status`,
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
